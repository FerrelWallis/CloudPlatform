#!usr/bin/perl -w
use strict;use Getopt::Long;use FindBin qw($Bin $Script);       use POSIX;
=head1 Version
Author: Zeng Liang,zengliang@biozeron.com
Date: 2020-01-24
=head1 Usage
-len        <int>       cut fq length ; work for format 1 , 6; default : 500
-prefix     <str>       work for -format 4 ,fa prefix id
-format     <int>  *    1: stat fq len;
                        2: filter fq len;
                        3: stat fq,N50 N90, large len
                        4: fq 2 fa
                        5: check read1 read2 fq id
                        6: cut fq length;  
-help                   output help information to screen
=head1 Exmple
perl deal_fq.pl fq/fq.gz -len 500 -format 2
perl deal_fq.pl fq/fq.gz -prefix s -format 4
perl deal_fq.pl fq1/fq1.gz fq2/fq2.gz -format 5
perl deal_fq.pl fq1/fq1.gz -len 101 -format 6 
=cut
my ($type, $format, $help, $len, $prefix, $N, $win, $index, $sysn);
GetOptions(
	"format:i"=>\$format,
	"len:i"=>\$len,
        "prefix:s"=>\$prefix,
	"help"=>\$help
);
die `pod2text $0` if (@ARGV < 1 ||$help);	$len ||=500;
if($ARGV[0]=~/.gz$/){
	open A,"gzip -dc $ARGV[0] |";
}
else{
	open A,$ARGV[0];
}
if($format == 1){
	while(<A>){
		my $inf=<A>;  chomp $inf;my $num=length $inf;my @a=split /\s+/;
		print "$a[0]\t$num\n";
		<A>;<A>;
	}
}
if($format == 2){
	while(<A>){
		my $inf=<A>;  chomp $inf;my $num=length $inf;
		next if($num < $len);		my $p1=<A>;	my $p2=<A>;
		print "$_$inf\n$p1$p2";
	}
}
if($format == 3){
	my $large=0;    my $gc=0;       my $small=10000000000000000000000;my $num=0;
	my $n50=0;      my $l50=0;      my $n90=0;  my $l90=0;          my %seq;        my $total=0;my $GC=0;my $nn=0;
	while(<A>){
		chomp;my $inf=<A>; chomp $inf; my $sub_len=length $inf; my @b=split /\s+/,$_; my $seq=$inf; $b[0]=~s/^@//;
		$seq{$b[0]}=$sub_len;       $total+=$sub_len;   $num++;
		$GC+=0+($seq=~s/G/G/g);  $GC+=0+($seq=~s/C/C/g);
		$GC+=0+($seq=~s/g/g/g);  $GC+=0+($seq=~s/c/c/g);
		$nn+=0+($seq=~s/N/N/g);  $nn+=0+($seq=~s/n/n/g);
		if($sub_len > $large){                              $large=$sub_len;                    }
		if($sub_len < $small){                              $small=$sub_len;                    }
		<A>; <A>;

	}
	my $acc1=0;     my $acc2=0;
	foreach my $k(sort {$seq{$b}<=>$seq{$a}} keys %seq){
		$acc1+=$seq{$k};
		if($acc1/$total >= 0.5){
			$n50=$seq{$k};  $l50=$k;        last;
		}
	}
	foreach my $k(sort {$seq{$b}<=>$seq{$a}} keys %seq){
		$acc2+=$seq{$k};
		if($acc2/$total >= 0.9){
			$n90=$seq{$k};  $l90=$k;    last;
		}
	}
	close A;my $gc_rate=int(10000*$GC/$total)/100; my $nn_rate=$nn/$total; my $aver=int($total/$num);
	print "Total length:\t$total\nTotal number:\t$num\nAver len:\t$aver\nGC rate:\t$gc_rate\nmax length:\t$large\n";
	print "min length:\t$small\nN50:\t$n50\nL50:\t$l50\nN90:\t$n90\nL90:\t$l90\nN rate:\t$nn_rate\n";
}
if($format == 4){
        while(<A>){
                my $inf=<A>;  my $p1=<A>;     my $p2=<A>;
		print ">$prefix$.\n$inf";
        }
}
if($format == 5){
	if($ARGV[1] =~ /\.gz$/){
        	open B,"gzip -dc $ARGV[1] |" or die "$!\n";
	}else{
	        open B,$ARGV[1] or die "$!\n";
	}
	while(<A>){
        	chomp;  my @b=split /\s+/;      <A>;    <A>;    <A>;
	        my $pp1=<B>;    <B>;<B>;<B>;
        	my @pp=split /\s+/,$pp1;
	        if($b[0] ne $pp[0]){
                	print "$_\t$pp1";
        	}
	}
	close A;
}
if($format == 6){
	while(my $fqa1=<A>){
        	my $fqa2=<A>; my $fqa3=<A>; my $fqa4=<A>;
	        chomp $fqa2;    chomp $fqa4;
        	my $sub_fqa2=substr($fqa2,0,$len);  my $sub_fqa4=substr($fqa4,0,$len);
	        my $lennn=length $fqa2;
        	print "$fqa1$sub_fqa2\n$fqa3$sub_fqa4\n";
	}

}
