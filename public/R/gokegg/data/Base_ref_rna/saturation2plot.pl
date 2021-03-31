#!/usr/bin/perl -w

use warnings;
use Getopt::Long;
use Time::Local;
my %opts;
GetOptions (\%opts,"in=s","out=s","h!");

my $usage = <<"USAGE";
        Program : $0
	contact :binxu.liu\@majorbio.com
        Discription:saturation2plot.pl
        Usage:perl $0 [options]
		-in		*.eRPKM.xls
		-out 		output index
        example:perl $0
USAGE

die $usage if ( !$opts{in} || !$opts{out} || $opts{h} );

my $outindex=$opts{out};

my $samfile;
my @freq=(5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100);
my @rpkm_bind=(0,0.3,0.6,3.5,15,60);
my %lib_size;
my %gene_rpkm;
my %gene_deviation;
my %gene_cluster;
my %gene_cluster_percent;

my %genome;


#caculate rpkm
open (RPKM,"<$opts{in}") || die "Can not open rpkm.xls\n";
while(<RPKM>){
	chomp;
	if($_ =~ /start/){
		next;
	}
	my @line = split (/\t/,$_);
	my $gene = $line[3];
	my $i=1;
	foreach(@freq){
		my $fre=$_;
		$gene_rpkm{$gene}{$fre}=$line[5+$i];
		$i++;
		# print RPKM "\t".$gene_rpkm{$gene}{$fre};
	}
	
}
close RPKM;

#caculate deviation
open (DEV,">$outindex.deviation.xls") ||die "Can not open deviation.xls\n";
foreach(keys %gene_rpkm){
	my $gene = $_;
	print DEV $gene;
	foreach(@freq){
		my $fre=$_;
		if ($gene_rpkm{$gene}{100} == 0){
			$gene_deviation{$gene}{$fre} = 1;
		}else{
			$gene_deviation{$gene}{$fre} = abs($gene_rpkm{$gene}{$fre} - $gene_rpkm{$gene}{100})/$gene_rpkm{$gene}{100};
		}
		print DEV "\t".$gene_deviation{$gene}{$fre};
	}
	print DEV "\n";
}
close DEV;

#cluster bindary
my $num=1;
foreach(@rpkm_bind){
	my $left=$_;	
	my $right="max";
	my $name="";
	my $des="";
	if (exists $rpkm_bind[$num]){
		$right=$rpkm_bind[$num];
		$name=$num;
		$des="[".$left."-".$right.")";
	}else{
		$name=$num;
		$des='>='.$left;
	}
	$gene_cluster{$name}{left}=$left;
	$gene_cluster{$name}{right}=$right;
	$gene_cluster{$name}{num}=0;
	$gene_cluster{$name}{des}=$des;

	$num++;	
}

#get cluster num
foreach(keys %gene_deviation){
	my $gene=$_;
	if ($gene_rpkm{$gene}{100}==0){
			next;
	}
	foreach(keys %gene_cluster){
		my $name=$_;		
		if($gene_cluster{$name}{right} eq "max" && $gene_rpkm{$gene}{100} >= $gene_cluster{$name}{left}){
			$gene_cluster{$name}{num}++;
			foreach(@freq){
				my $fre=$_;
				if ($gene_deviation{$gene}{$fre}<=0.15){
					if (exists $gene_cluster{$name}{$fre}){
						$gene_cluster{$name}{$fre} += 1;
					}else{
						$gene_cluster{$name}{$fre} = 1;
					}
				}
			}
		}elsif($gene_rpkm{$gene}{100} >= $gene_cluster{$name}{left} && $gene_rpkm{$gene}{100} < $gene_cluster{$name}{right}){
			$gene_cluster{$name}{num}++;
			foreach(@freq){
				my $fre=$_;
				if ($gene_deviation{$gene}{$fre}<=0.15){
					if (exists $gene_cluster{$name}{$fre}){
						$gene_cluster{$name}{$fre} += 1;
					}else{
						$gene_cluster{$name}{$fre} = 1;
					}
				}
			}
		}		
	}	
}

open (CLUSTER,">$outindex.cluster_percent.xls") ||die "Can not open cluster_percent.xls\n";

foreach(sort keys %gene_cluster){
	my $name=$_;
	print CLUSTER $gene_cluster{$name}{des};
	foreach(@freq){
		my $fre=$_;
		if($gene_cluster{$name}{num}==0){
			$gene_cluster_percent{$name}{$fre}=0;
		}else{
			if(!(exists $gene_cluster{$name}{$fre})){
				$gene_cluster{$name}{$fre}=0;
			}
			$gene_cluster_percent{$name}{$fre}=$gene_cluster{$name}{$fre}/$gene_cluster{$name}{num};
		}
		print CLUSTER "\t".$gene_cluster_percent{$name}{$fre};
	}
	print CLUSTER "\n";
}
close CLUSTER;

open (RSCRIPT,">$outindex.saturation.R") ||die "Can not open saturation.R\n";
print RSCRIPT 'x=c(5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100)'."\n";
print RSCRIPT 'col=rainbow(6)'."\n";
print RSCRIPT 'pdf(file="'.$outindex.'.saturation.pdf")'."\n";

my @legend;

foreach(sort keys %gene_cluster_percent){
	my $name=$_;
	my @per;
	my $leg='"'.$gene_cluster{$name}{des}."\tnum=".$gene_cluster{$name}{num}.'"';
	push(@legend,$leg);
	foreach(@freq){
		my $fre=$_;
		push(@per,$gene_cluster_percent{$name}{$fre});
	}
	print RSCRIPT 'y=c('.join(",",@per).')'."\n";
	if($name!=1){
		print RSCRIPT 'par(new=TRUE)'."\n";
		print RSCRIPT 'plot(x, y, axes=FALSE,ylim=c(0,1),xlim = c(0,110), xlab="", ylab="",pch=17,col=col['.$name.'],cex=1.5,cex.axis=1.5,cex.lab=1.5)'."\n";
	}else{
		print RSCRIPT 'plot(x, y, pch=17,col=col['.$name.'],ylim=c(0,1),xlim = c(0,110),xlab="mapped reads(%)", ylab="genes fpkm deviation within 15% of final value",cex=1.5,cex.axis=1.5,cex.lab=1.5)'."\n";
		print RSCRIPT 'grid(col = "gray")'."\n";
	}
	print RSCRIPT 'lines(x,y,col=col['.$name.'])'."\n";
}

print RSCRIPT 'legend(70, 0.24, legend=c('.join(",",@legend).'), cex=1, col=col,pch=17,lty=1)'."\n";
print RSCRIPT 'dev.off()'."\n";
close RSCRIPT;

process_cmd("R --no-save <  $outindex.saturation.R");


sub process_cmd {
    my ($cmd) = @_;
    print "CMD: $cmd\n";
    my $ret = system($cmd);
    if ($ret){
        die "Error, cmd: $cmd died with ret ($ret) ";
    }
    return;
}


