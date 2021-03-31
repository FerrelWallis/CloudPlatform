#!usr/bin/perl
use strict;
#use warnings;
die "sam1\tinfo1\nsam2\tinfo2\nusage:perl $0 <snp.list> <head(yes or not)> <output file>\n" unless(@ARGV==3);
open B,$ARGV[0];my %hash;my %all;   open OUT,">$ARGV[2]"; open A,"<$ARGV[2]";
my @am=qw /A C/;my @ar=qw /A G/;my @aw=qw /A T/;my @ay=qw /C T/;my @as=qw /C G/;my @ak=qw /G T/;
$hash{"M"}=\@am;$hash{"R"}=\@ar;$hash{"W"}=\@aw;$hash{"Y"}=\@ay;$hash{"S"}=\@as;$hash{"K"}=\@ak;
my $rec=0;my @sam;
while(<B>){
	chomp;my @bb=split /\s+/;push @sam,$bb[0];    open A,$bb[1];  
	if($ARGV[1] eq "yes"){        <A>;    }
	my (%hash_mu,%hash_stat);$rec++;
	while(my $inf=<A>){
		chomp $inf;
		my @a=split /\s+/,$inf;
		if($a[5]=~/[ATCG]/i){
			my $vector=$a[3]."->".$a[5];
			$hash_mu{$vector}+=2;
		}
		else{
			my $vector=$a[3]."->".$hash{$a[5]}->[0];
			$hash_mu{$vector}++;
			$vector=$a[3]."->".$hash{$a[5]}->[1];
			$hash_mu{$vector}++;
		}
	}
	$hash_stat{'C:G->T:A'}=$hash_mu{"C->T"}+$hash_mu{"G->A"};$all{'C:G->T:A'}[0]+=$hash_mu{"C->T"}+$hash_mu{"G->A"};
	$hash_stat{'C:G->A:T'}=$hash_mu{"C->A"}+$hash_mu{"G->T"};$all{'C:G->A:T'}[0]+=$hash_mu{"C->A"}+$hash_mu{"G->T"};
	$hash_stat{'C:G->G:C'}=$hash_mu{"C->G"}+$hash_mu{"G->C"};$all{'C:G->G:C'}[0]+=$hash_mu{"C->G"}+$hash_mu{"G->C"};
	$hash_stat{'T:A->C:G'}=$hash_mu{"T->C"}+$hash_mu{"A->G"};$all{'T:A->C:G'}[0]+=$hash_mu{"T->C"}+$hash_mu{"A->G"};
	$hash_stat{'T:A->G:C'}=$hash_mu{"T->G"}+$hash_mu{"A->C"};$all{'T:A->G:C'}[0]+=$hash_mu{"T->G"}+$hash_mu{"A->C"};
	$hash_stat{'T:A->A:T'}=$hash_mu{"T->A"}+$hash_mu{"A->T"};$all{'T:A->A:T'}[0]+=$hash_mu{"T->A"}+$hash_mu{"A->T"};

	$all{'C:G->T:A'}[$rec]+=$hash_mu{"C->T"}+$hash_mu{"G->A"};
	$all{'C:G->A:T'}[$rec]+=$hash_mu{"C->A"}+$hash_mu{"G->T"};
	$all{'C:G->G:C'}[$rec]+=$hash_mu{"C->G"}+$hash_mu{"G->C"};
	$all{'T:A->C:G'}[$rec]+=$hash_mu{"T->C"}+$hash_mu{"A->G"};
	$all{'T:A->G:C'}[$rec]+=$hash_mu{"T->G"}+$hash_mu{"A->C"};
	$all{'T:A->A:T'}[$rec]+=$hash_mu{"T->A"}+$hash_mu{"A->T"};

}
close B;
my $head=join "\t",@sam;
print OUT "type\tall\t$head\n";
foreach my $k1(sort keys %all){
	print OUT "$k1";
	foreach my $k2(0..$rec){
		print OUT "\t$all{$k1}[$k2]";
	}
	print OUT "\n";
}
close OUT;
#draw by R
open A, "<$ARGV[2]";
# my %hash;
my $head=<A>;	my @head=split /\s+/,$head;
open OUT,">cmd.r";
foreach my $k(1..$#head){
	my $i=$k+1;	
	print  OUT "pdf(\"$head[$k].mutation.pdf\")
library(ggplot2)
data <-read.table(file=\"$ARGV[2]\",header=TRUE)
SNP_mutation<-as.matrix(data[,1])
number <-as.matrix(data[,$i])
mutation_type<-c(\"Transversion\",\"Transversion\",\"Transition\",\"Transversion\",\"Transition\",\"Transversion\")
ggplot(data,aes(x=SNP_mutation,y=number,fill=mutation_type))+geom_bar(stat=\"identity\",width=0.3)+scale_fill_manual(values=c(\"#E41A1C\",\"#1E90FF\"))+theme_bw()+theme(axis.text.x=element_text(angle=60,hjust=1))

dev.off()
";
}
`R --restore --no-save < cmd.r`;
close A;
