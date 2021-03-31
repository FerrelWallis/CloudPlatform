#!usr/bin/perl -w

use strict;

die "usage:perl $0 <deg.list> \n" unless @ARGV == 1;

open A,$ARGV[0];
print "group\ttype\tall\tup\tdown\n";
while(<A>){
	chomp;my @a=split;	open IN,$a[2];	my $up=0;	my $down=0;	my $all=0;
	my $head=<IN>;	my $ppp=-4;	chomp $head;	my @head=split /\t/,$head;
	foreach my $k(0..$#head){
		if($head[$k] eq "type"){
			$ppp=$k;
		}
	}
	while(my $inf=<IN>){
		chomp $inf;my @b=split /\t/,$inf;		$all++;
		if($b[$ppp] eq "up"){			$up++;		}
		if($b[$ppp] eq "down"){			$down++;	}
	}
	close IN;
	print "$a[0]\t$a[1]\t$all\t$up\t$down\n";
}
close A;
